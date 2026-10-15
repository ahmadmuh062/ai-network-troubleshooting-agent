
package com.networkagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NetworkLogRequest(

        @NotBlank(message = "Device name is required")
        @Size(
                max = 100,
                message = "Device name must not exceed 100 characters"
        )
        String deviceName,

        @NotBlank(message = "Device type is required")
        @Size(
                max = 50,
                message = "Device type must not exceed 50 characters"
        )
        String deviceType,

        @NotBlank(message = "Log content is required")
        @Size(
                max = 20000,
                message = "Log content exceeds maximum length"
        )
        String logContent

) {
}
