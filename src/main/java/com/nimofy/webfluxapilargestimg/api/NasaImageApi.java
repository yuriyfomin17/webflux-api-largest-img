package com.nimofy.webfluxapilargestimg.api;

import com.nimofy.webfluxapilargestimg.dto.Photos;
import com.nimofy.webfluxapilargestimg.service.NasaImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@RestController
@RequestMapping("/nasaImage")
public class NasaImageApi {
    private final NasaImageService nasaImageService;

    @GetMapping("/max/image/{sol}")
    public ResponseEntity<?> getMaxImageBySol(@PathVariable int sol) {
        var body = nasaImageService.getLargestImageBySol(sol);
        return ResponseEntity.ok()
                .contentLength(body.length)
                .contentType(MediaType.IMAGE_JPEG)
                .body(body);
    }
    @GetMapping(value = "max/image/reactive/{sol}", produces = MediaType.IMAGE_PNG_VALUE)
    public Mono<byte[]> getMaxImageBySolReactive(@PathVariable int sol){

        return nasaImageService.getLargestImageBySolReactive(sol);
    }
}
