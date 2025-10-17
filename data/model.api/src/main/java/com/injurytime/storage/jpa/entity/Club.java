package com.injurytime.storage.jpa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "club")
public class Club {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "CLUB_ID")
  private Integer id;                      // surrogate PK

  @Column(name = "API_CLUB_ID", nullable = false, unique = true)
  private Integer apiClubId;               // external ID

  @Column(name = "CLUB_NAME", nullable = false, unique = true, length = 100)
  private String clubName;

  @Column(name = "CLUB_ABBR", nullable = false, unique = true, length = 3) // 3-letter code; use 4 if you prefer
  private String clubAbbr;

  @Column(name = "COUNTRY_CODE", length = 3)
  private String countryCode;

  @Column(name = "LOGO_URL")
  private String logoUrl;

  @Column(name = "CREATED_AT", insertable = false, updatable = false)
  private java.time.Instant createdAt;

  @Column(name = "UPDATED_AT", insertable = false, updatable = false)
  private java.time.Instant updatedAt;

 
    public Club()
    {
    }

    public Integer getId()
    {
        return id;
    }

    public Integer getApiClubId()
    {
        return apiClubId;
    }

    public void setApiClubId(Integer v)
    {
        this.apiClubId = v;
    }

    public String getClubName()
    {
        return clubName;
    }

    public void setClubName(String v)
    {
        this.clubName = v;
    }

    public String getClubAbbr()
    {
        return clubAbbr;
    }

    public void setClubAbbr(String v)
    {
        this.clubAbbr = v;
    }

    public String getCountryCode()
    {
        return countryCode;
    }

    public void setCountryCode(String v)
    {
        this.countryCode = v;
    }

    public String getLogoUrl()
    {
        return logoUrl;
    }

    public void setLogoUrl(String v)
    {
        this.logoUrl = v;
    }
}
