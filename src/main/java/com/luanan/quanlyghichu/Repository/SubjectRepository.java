package com.luanan.quanlyghichu.Repository;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.luanan.quanlyghichu.Model.DTO.Response.SubjectDTO;
import com.luanan.quanlyghichu.Model.Entities.Subject;

public interface SubjectRepository extends JpaRepository<Subject, Integer>{

	@Query(value = "select * from Subject s where s.id_timetable in :idtimetable and lower(s.name) like %:name% and lower(s.class_code) like %:classcode%"
			+ " and lower(s.type) in :type", nativeQuery = true)
	List<Subject> findByTimeTable(List<Integer> idtimetable,String name, String classcode, String[] type);
	
	@Modifying
	@Query(value = "DELETE FROM Subject WHERE id_timetable = :idtimetable", nativeQuery = true)
	@Transactional
	int deleteSubject(@Param("idtimetable") int idtimetable);
	
	@Query(value = "select NEW com.luanan.quanlyghichu.Model.DTO.Response.SubjectDTO(" +
			"s.id,s.code,s.name,s.classCode,s.day,s.start,s.number,s.room,s.note,s.type,s.timetable.week) " +
			"from Subject s " +
			"where s.id = :id ", nativeQuery = false)
	SubjectDTO getById(@Param("id")int id);
}
