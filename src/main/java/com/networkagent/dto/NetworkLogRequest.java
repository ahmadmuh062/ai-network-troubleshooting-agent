
package com.networkagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NetworkLogRequest(

        @NotBlank(message = "Device name is required")
        String deviceName,

        @NotBlank(message = "Device type is required")
        String deviceType,

        @NotBlank(message = "Log content is required")
        @Size(max = 20000,
                message = "Log content exceeds maximum length")
        String logContent

) {
}
