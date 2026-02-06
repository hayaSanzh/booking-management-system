package com.booking.dto.resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceFilterRequest {
    
    private String name;
    private String location;
    private Integer capacityMin;
    private Boolean isActive;
}
