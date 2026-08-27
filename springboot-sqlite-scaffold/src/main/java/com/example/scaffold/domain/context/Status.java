package com.example.scaffold.domain.context;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "status")
public class Status {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "status", nullable = false, unique = true)
	private Integer status;

	@Column(name = "description", nullable = false)
	private String description;
}
