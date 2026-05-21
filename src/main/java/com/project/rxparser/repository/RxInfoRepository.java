package com.project.rxparser.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.project.rxparser.model.RxInfo;


@Repository
public interface RxInfoRepository extends JpaRepository<RxInfo, Long> {

	// Get current existing rx info to compare for duplicates while saving in RxServiceImpl.processFile
	@Query("SELECT r.rx FROM RxInfo r WHERE r.member.memberId = :memberId AND r.rx IN :rxList")
    Set<String> findExistingRxByMemberAndRxList(Long memberId, List<String> rxList);
}
