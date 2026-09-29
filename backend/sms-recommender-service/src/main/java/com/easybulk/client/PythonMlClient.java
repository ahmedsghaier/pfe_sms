package com.easybulk.client;

import com.easybulk.config.FeignConfig;
import com.easybulk.dto.FeedbackRequest;
import com.easybulk.dto.RecommendRequest;
import com.easybulk.dto.RecommendResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name  = "python-ml-client",
        url   = "${ml.python-service-url}",
        configuration = FeignConfig.class
)
public interface PythonMlClient {

    @PostMapping("/recommend")
    RecommendResponse recommend(@RequestBody RecommendRequest request);

    @PostMapping("/feedback")
    void feedback(@RequestBody FeedbackRequest request);
}