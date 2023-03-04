package com.nimofy.webfluxapilargestimg.service;

import com.nimofy.webfluxapilargestimg.dto.Image;
import com.nimofy.webfluxapilargestimg.dto.Photos;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class NasaImageService {
    private final RestTemplate restTemplate;
    @Value("${nasa.api.url}")
    private String NASA_URL;
    @Value("${nasa.api.key}")
    private String NASA_KEY;

    public byte[] getLargestImageBySol(int sol) {
        // todo: find max image by sol
        // todo: return byte of max image
        var nasaImages = restTemplate.getForEntity(buildURI(sol), Photos.class).getBody();
        assert nasaImages != null;
        var maxImage = nasaImages.photos()
                .parallelStream()
                .map(imageSrc -> createImageFromUrl(imageSrc.img_src()))
                .max(Comparator.comparing(Image::size)).orElseThrow();
        return getImageByte(maxImage);
    }

    public Mono<byte[]> getLargestImageBySolReactive(int sol) {
        return WebClient.create(buildURI(sol).toString())
                .get()
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(resp -> resp.bodyToMono(Photos.class))
                .flatMapMany(photos -> Flux.fromIterable(photos.photos()))
                .flatMap(imageSrc ->
                        WebClient.create(imageSrc.img_src())
                                .head()
                                .exchangeToMono(ClientResponse::toBodilessEntity)
                                .map(HttpEntity::getHeaders)
                                .map(HttpHeaders::getLocation)
                                .map(URI::toString)
                                .flatMap(redirectedPictureUrl -> WebClient.create(redirectedPictureUrl)
                                        .head()
                                        .exchangeToMono(ClientResponse::toBodilessEntity)
                                        .map(HttpEntity::getHeaders)
                                        .map(HttpHeaders::getContentLength)
                                        .map(size -> new Image(redirectedPictureUrl, size))
                                )

                ).reduce((p1, p2) -> p1.size() > p2.size() ? p1 : p2)
                .flatMap(largestImage ->
                        WebClient.create(largestImage.url())
                                .mutate().codecs(configs -> configs.defaultCodecs().maxInMemorySize(10_000_000))
                                .build()
                                .get()
                                .exchangeToMono(resp -> resp.bodyToMono(byte[].class)));
    }

    private byte[] getImageByte(Image image) {
        return restTemplate.getForObject(image.url(), byte[].class);
    }

    private Image createImageFromUrl(String imgSrc) {
        var headersInfo = restTemplate.headForHeaders(imgSrc);
        var location = headersInfo.getLocation();
        assert location != null;
        var imageInfo = restTemplate.headForHeaders(location);
        var size = imageInfo.getContentLength();
        return new Image(location.toString(), size);
    }

    private URI buildURI(int sol) {
        return UriComponentsBuilder.fromHttpUrl(NASA_URL)
                .queryParam("sol", sol)
                .queryParam("api_key", NASA_KEY)
                .build().toUri();
    }

}
