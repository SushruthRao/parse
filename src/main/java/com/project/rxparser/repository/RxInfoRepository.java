package com.project.rxparser.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.rxparser.model.RxInfo;


@Repository
public interface RxInfoRepository extends JpaRepository<RxInfo, Long> {

}
