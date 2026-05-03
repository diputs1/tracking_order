package com.example.tracking_order.common.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageData<T> {
    private List<T> items;
    private Pagination pagination;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pagination {
        private int page;
        
        @JsonProperty("total_pages")
        private int totalPages;
        
        @JsonProperty("total_items")
        private long totalItems;
    }
}
