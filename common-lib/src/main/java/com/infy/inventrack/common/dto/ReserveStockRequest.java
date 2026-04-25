package com.infy.inventrack.common.dto; import jakarta.validation.constraints.*; import lombok.Data; @Data public class ReserveStockRequest { @NotNull @Min(1) private Integer quantity; }
