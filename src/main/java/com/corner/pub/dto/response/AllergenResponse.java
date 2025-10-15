package com.corner.pub.dto.response;

public class AllergenResponse {
    private String code;
    private String label;
    private String status;   // CONTAINS | MAY_CONTAIN
    private String iconUrl;  // Cloudinary URL

    public String getCode(){ return code; }
    public void setCode(String code){ this.code = code; }
    public String getLabel(){ return label; }
    public void setLabel(String label){ this.label = label; }
    public String getStatus(){ return status; }
    public void setStatus(String status){ this.status = status; }
    public String getIconUrl(){ return iconUrl; }
    public void setIconUrl(String iconUrl){ this.iconUrl = iconUrl; }
}