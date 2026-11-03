package com.seinfo.seinfomationA.util.security;
public final class BusinessVerification {
    private BusinessVerification() {}
    public static String key(String number,String ceo,String start,String company) {
        return PasswordVersion.of(String.join("\n",number.trim(),ceo.trim(),start.replace("-","").trim(),company.trim()));
    }
}
