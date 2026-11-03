package com.seinfo.seinfomationA;

import com.seinfo.seinfomationA.util.mybatis.CamelMap;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MyBatisIdFeatureTest {
    Configuration mapper(String path) throws Exception {
        Configuration config = new Configuration(); config.getTypeAliasRegistry().registerAlias("camelmap", CamelMap.class);
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in); new XMLMapperBuilder(in, config, path, config.getSqlFragments()).parse();
        }
        return config;
    }
    @Test void loginQueryUsesPublicIdAndEmailLookupStillExists() throws Exception {
        Configuration c = mapper("mybatis/main/login.xml");
        String n = "com.seinfo.seinfomationA.component.main.mapper.LoginMapper.";
        String sql = c.getMappedStatement(n+"selectUsrInfo").getBoundSql(Map.of("loginId", "member19")).getSql();
        assertTrue(sql.contains("a.login_id = ?"));
        String email = c.getMappedStatement(n+"selectUserByEmail").getBoundSql("registered@example.invalid").getSql();
        assertTrue(email.contains("WHERE user_email = ?"));
    }
    @Test void authorSearchAndPaginationUseTheSameIdPredicate() throws Exception {
        Configuration c = mapper("mybatis/board/freeboard.xml");
        String n = "com.seinfo.seinfomationA.component.board.mapper.FreeBoardMapper.";
        for (String id : new String[]{"selectFreeBoardList", "getFreeBoardListCount"}) {
            String sql = c.getMappedStatement(n+id).getBoundSql(Map.of("searchType", "n", "keyword", "member19", "pageSize",10,"startRow",0)).getSql();
            assertTrue(sql.contains("c.login_id LIKE")); assertFalse(sql.contains("c.user_nm LIKE"));
        }
        String detail = c.getMappedStatement(n+"selectFreeBoardDetail").getBoundSql(Map.of("freeboardNo","test")).getSql();
        assertTrue(detail.contains("AS author_id")); assertTrue(detail.contains("'관리자'"));
    }
    @Test void otherModifiedMappersParseAndTeamInsertIsNotAnUpsert() throws Exception {
        for (String path : new String[]{"mybatis/member/member.xml", "mybatis/admin/manageTeam.xml", "mybatis/request/request.xml",
                "mybatis/admin/admin.xml", "mybatis/board/qna.xml", "mybatis/board/worklog.xml"}) mapper(path);
        Configuration c = mapper("mybatis/admin/manageTeam.xml");
        String sql = c.getMappedStatement("com.seinfo.seinfomationA.component.admin.mapper.ManageTeamMapper.insertTeamMember")
                .getBoundSql(Map.of()).getSql();
        assertFalse(sql.contains("ON DUPLICATE KEY")); assertTrue(sql.contains("login_id"));
    }
}
