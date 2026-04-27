package com.example.dataplatform.control.dto;

import java.util.List;

public record DlqReplayRequest(
        List<String> ids,
        String actor
) {
}
