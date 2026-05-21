package com.project.rxparser.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;


@Entity
@Table(name = "rx_info")
@Data
public class RxInfo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="rx_id", nullable = false)
	private Long rxId;
	
	@Column(name="rx", nullable = false)
	private String rx;
	
	@Column(name="drugName", nullable = false)
    private String drugName;
	
	@Column(name="description", nullable = false)
    private String description;

	@ManyToOne
	@JoinColumn(name = "member_id", nullable = false)
	private MembershipInfo member;

}
