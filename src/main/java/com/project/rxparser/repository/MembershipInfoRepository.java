package com.project.rxparser.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.rxparser.model.MembershipInfo;


@Repository
public interface MembershipInfoRepository extends JpaRepository<MembershipInfo, Long> {

}
