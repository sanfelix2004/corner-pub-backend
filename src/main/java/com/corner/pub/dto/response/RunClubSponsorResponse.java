package com.corner.pub.dto.response;

import com.corner.pub.model.RunClubSponsor;
import com.corner.pub.service.StorageService;

public class RunClubSponsorResponse {

    private Long id;
    private String name;
    private String socialUrl;
    private String imageUrl;

    public RunClubSponsorResponse() {
    }

    public RunClubSponsorResponse(RunClubSponsor sponsor) {
        this.id = sponsor.getId();
        this.name = sponsor.getName();
        this.socialUrl = sponsor.getSocialUrl();
        this.imageUrl = StorageService.toLocalUrl(sponsor.getImagePath());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSocialUrl() { return socialUrl; }
    public void setSocialUrl(String socialUrl) { this.socialUrl = socialUrl; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
