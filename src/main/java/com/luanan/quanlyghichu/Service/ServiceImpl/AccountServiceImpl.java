package com.luanan.quanlyghichu.Service.ServiceImpl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.luanan.quanlyghichu.Model.DTO.Request.ChangePasswordDTO;
import com.luanan.quanlyghichu.Model.DTO.Request.ChangePasswordFirstDTO;
import com.luanan.quanlyghichu.Model.DTO.Request.EditAccountDTO;
import com.luanan.quanlyghichu.Model.DTO.Request.LoginDTO;
import com.luanan.quanlyghichu.Model.DTO.Request.SignupDTO;
import com.luanan.quanlyghichu.Model.DTO.Response.LoginResponse;
import com.luanan.quanlyghichu.Model.DTO.Response.ResponseModel;
import com.luanan.quanlyghichu.Model.Entities.Account;
import com.luanan.quanlyghichu.Model.Entities.Role;
import com.luanan.quanlyghichu.Repository.AccountRepository;
import com.luanan.quanlyghichu.Security.JwtUtils;
import com.luanan.quanlyghichu.Security.Service.UserDetailsImpl;
import com.luanan.quanlyghichu.Service.AccountService;

@Service
public class AccountServiceImpl implements AccountService{
	
	final AccountRepository accountRepository;
	final PasswordEncoder passwordEncoder;
	final ModelMapper modelMapper;
	final AuthenticationManager authenticationManager;
	final JwtUtils jwtUtils;
	private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();


	public AccountServiceImpl(AccountRepository accountRepository, PasswordEncoder passwordEncoder,
			ModelMapper modelMapper, AuthenticationManager authenticationManager, JwtUtils jwtUtils,
			BCryptPasswordEncoder encoder) {
		super();
		this.accountRepository = accountRepository;
		this.passwordEncoder = passwordEncoder;
		this.modelMapper = modelMapper;
		this.authenticationManager = authenticationManager;
		this.jwtUtils = jwtUtils;
		this.encoder = encoder;
	}

	@Override
	public ResponseEntity<?> signup(SignupDTO dto) {
		Optional<Account> optional = accountRepository.findByUsername(dto.getUsername());
		if(optional.isPresent()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseModel(
					"Tên đăng nhập đã tồn tại",400));
		}
		if((dto.getRole().equals("admin")) && (dto.getRole().equals("user"))) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseModel(
					"Không tồn tại quyền đã nhập",400));
		}
		Account newAccount = modelMapper.map(dto, Account.class);
		newAccount.setPassword(encoder.encode(dto.getPassword()));
		if(dto.getStatus() == 1) {
			newAccount.setStatus(true);
		}else {
			newAccount.setStatus(false);
		}
		
		Role role = Role.valueOf(dto.getRole().toLowerCase());
		
		newAccount.setRole(role);
		newAccount.setFirstLogin(true);
		accountRepository.save(newAccount);
		return ResponseEntity.ok().body(new ResponseModel("Đăng ký thành công",200));
	}

	@Override
	public ResponseEntity<?> login(LoginDTO dto) {
		Optional<Account> optional = accountRepository.findByUsername(dto.getUsername());
		if(!optional.isPresent()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseModel(
					"Tài khoản không tồn tại",404,dto));
		}
		if(!optional.get().isStatus()== true) {
			return ResponseEntity.status(HttpStatus.OK).body(new ResponseModel(
					"Tài khoản đã bị khóa",200));
		}
		if(!BCrypt.checkpw(dto.getPassword(),optional.get().getPassword())){
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseModel(
					"Mập khẩu không đúng",404,dto));
		}
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));
		SecurityContextHolder.getContext().setAuthentication(authentication);
		String jwt = jwtUtils.generateJwtToken(authentication);
		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		List<String> roles = userDetails.getAuthorities().stream().map(item -> item.getAuthority())
				.collect(Collectors.toList());
		return ResponseEntity.ok(new ResponseModel("Đăng nhập thành công",200,new LoginResponse(jwt, 
				userDetails.getId(), userDetails.getUsername(), roles.get(0), userDetails.isStatus(), userDetails.isFirstLogin())));
	}

	@Override
	public ResponseEntity<?> changePassword(ChangePasswordDTO dto) {
		Optional<Account> optional = accountRepository.findById(dto.getAccountid());
		if(optional.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseModel(
					"Tài khoản không tồn tại",404,dto));
		}
		if(!BCrypt.checkpw(dto.getOldPassword(),optional.get().getPassword())) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseModel(
					"Mập khẩu cũ không đúng",404,dto));
		}
		Account account = optional.get();
		account.setPassword(encoder.encode(dto.getNewPassword()));
		Account newAccount = accountRepository.save(account);
		return ResponseEntity.ok().body(new ResponseModel("Thay đổi mật khẩu thành công",200));
	}

	@Override
	public ResponseEntity<?> changePasswordFirstLogin(ChangePasswordFirstDTO dto) {
		Optional<Account> optional = accountRepository.findById(dto.getAccountid());
		if(optional.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseModel(
					"Tài khoản không tồn tại",404,dto));
		}
		Account account = optional.get();
		account.setPassword(encoder.encode(dto.getNewPassword()));
		account.setFirstLogin(false);
		Account newAccount = accountRepository.save(account);
		return ResponseEntity.ok().body(new ResponseModel("Thay đổi mật khẩu thành công",200));
	}

	@Override
	public ResponseEntity<?> editAccount(EditAccountDTO dto) {
		Optional<Account> optional = accountRepository.findById(dto.getAccountid());
		if(optional.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseModel(
					"Tài khoản không tồn tại",404,dto));
		}
		Account account = optional.get();
		account.setStatus(dto.getStatus() == 1);
		Account newAccount = accountRepository.save(account);
		return ResponseEntity.ok().body(new ResponseModel("Thay đổi thành công",200,newAccount));
	}

	@Override
	public ResponseEntity<?> getListAccount() {
		List<Account> accounts = accountRepository.findAll();
		return ResponseEntity.ok().body(new ResponseModel("Danh sách tài khoản",200,accounts));
	}

}
