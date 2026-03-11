package com.chimera.actservice.publisher;

import com.chimera.actservice.client.dto.ContentBundleDto;

public interface ContentPublisher {

    String platform();

    String publish(ContentBundleDto bundle);
}
