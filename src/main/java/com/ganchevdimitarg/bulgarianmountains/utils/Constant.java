package com.ganchevdimitarg.bulgarianmountains.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Constant {
    REGEX("""
            Местоположение:
            |GPS:
            |Описание:
            |Съседни обекти:
            |Изходен пункт:
            |Изходни пунктове:
            |Стопанин:
            |За контакти:
            |За контакти с хижата:
            |е-mail:
            |Тавански етаж:
            |Заслонът е пункт по маршрута:
            |url:
            |тел\\. :
            """),
    DOMAIN("https://www.btsbg.org"),
    FILE("tmp.txt"),
    FOR_CONTACT("За контакти:"),
    FIND_CONTACT_VARIANCE_REGEX("""
            За контакти с обекта:
            |За контакти с хижара - Николай Шмикеров:
            |За контакти с хижата:
            |За контакти :
            |За контакти на хижата :
            |За контакти с обекта:
            |за контакти.
            |За контакти и резервации:
            |Контакти за обекта :
            |За контакти с туристическото дружество:
            |e-mail:
            """),
    ALTITUDE_REGEX("(\\d{3,4})\\s*м\\s*\\.?\\s*н\\s*\\.??\\s*в\\s*\\.?"),
    SPLIT_DELIMITER(":");

    private final String constant;

    @Override
    public String toString() {
        return this.constant;
    }
}
