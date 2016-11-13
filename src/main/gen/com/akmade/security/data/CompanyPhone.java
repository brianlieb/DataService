package com.akmade.security.data;
// Generated Nov 6, 2016 11:40:20 AM by Hibernate Tools 5.2.0.Beta1

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

/**
 * CompanyPhone generated by hbm2java
 */
@Entity
@Table(name = "company_phone", catalog = "security")
public class CompanyPhone implements java.io.Serializable {

	private int companyId;
	private Company company;
	private String phone;
	private Date createdDate;
	private Date lastmodifiedDate;

	public CompanyPhone() {
	}

	public CompanyPhone(Company company, String phone, Date createdDate, Date lastmodifiedDate) {
		this.company = company;
		this.phone = phone;
		this.createdDate = createdDate;
		this.lastmodifiedDate = lastmodifiedDate;
	}

	@GenericGenerator(name = "generator", strategy = "foreign", parameters = @Parameter(name = "property", value = "company"))
	@Id
	@GeneratedValue(generator = "generator")

	@Column(name = "company_id", unique = true, nullable = false)
	public int getCompanyId() {
		return this.companyId;
	}

	public void setCompanyId(int companyId) {
		this.companyId = companyId;
	}

	@OneToOne(fetch = FetchType.LAZY)
	@PrimaryKeyJoinColumn
	public Company getCompany() {
		return this.company;
	}

	public void setCompany(Company company) {
		this.company = company;
	}

	@Column(name = "phone", nullable = false, length = 45)
	public String getPhone() {
		return this.phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created_date", nullable = false, length = 19)
	public Date getCreatedDate() {
		return this.createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "lastmodified_date", nullable = false, length = 19)
	public Date getLastmodifiedDate() {
		return this.lastmodifiedDate;
	}

	public void setLastmodifiedDate(Date lastmodifiedDate) {
		this.lastmodifiedDate = lastmodifiedDate;
	}

}
