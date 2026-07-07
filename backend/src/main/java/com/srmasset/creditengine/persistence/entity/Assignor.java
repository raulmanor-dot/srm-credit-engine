package com.srmasset.creditengine.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "assignors")
public class Assignor {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(name = "tax_id", nullable = false, unique = true)
	private String taxId;

	@Column(nullable = false)
	private boolean active = true;

	protected Assignor() {
	}

	public Assignor(String name, String taxId) {
		this.name = name;
		this.taxId = taxId;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getTaxId() {
		return taxId;
	}

	public boolean isActive() {
		return active;
	}
}
