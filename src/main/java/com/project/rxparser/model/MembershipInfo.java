package com.project.rxparser.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "membership_info")
@Data
public class MembershipInfo {

	@Id
	@Column(name="member_id", nullable = false)
	private Long memberId;
	
	@Column(name="firstname", nullable = false)
	private String firstName;
	
	@Column(name="lastname", nullable = false)
    private String lastName;
	
	@Column(name="dob", nullable = false)
    private String dob;
	
	@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<RxInfo> rxInfo = new ArrayList<>();
	
}
