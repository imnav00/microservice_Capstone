package com.infy.inventrack.common.dto; import jakarta.validation.constraints.*; import lombok.Data; @Data public class CategoryRequest { @NotBlank @Size(min=2,max=80) private String name; }
