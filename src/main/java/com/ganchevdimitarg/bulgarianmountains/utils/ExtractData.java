package com.ganchevdimitarg.bulgarianmountains.utils;

import com.ganchevdimitarg.bulgarianmountains.entites.Hut;
import com.ganchevdimitarg.bulgarianmountains.repositories.HutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExtractData implements CommandLineRunner {
    private final RestClient restClient;
    private final HutRepository hunRepository;

    @Override
    public void run(String... args) {
        List<Hut> huts = new ArrayList<>();
        String body = this.restClient.get()
                .uri("https://www.btsbg.org/hizhi")
                .retrieve()
                .body(String.class);

        Document doc = Jsoup.parse(Objects.requireNonNull(body));
        Elements cities = doc.select("h3");
        cities.forEach(city -> {
            String name = city.text();
            Element nextElement = city.nextElementSibling();

            while (nextElement != null && !nextElement.tagName().equals("h3")) {
                Elements hutLinks = nextElement.select("a[href*='/hizhi/']");

                for (Element link : hutLinks) {
                    String hutName = link.text();
                    String hutUrl = link.attr("href");

                    huts.add(Hut.builder().city(name).name(hutName).uri("https://www.btsbg.org" + hutUrl).build());
                }

                nextElement = nextElement.nextElementSibling();
            }
        });

        this.hunRepository.saveAll(huts);
        log.info("Saved {} huts", huts.size());
    }
}
