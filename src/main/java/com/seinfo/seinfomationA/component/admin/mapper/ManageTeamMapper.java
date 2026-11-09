package com.seinfo.seinfomationA.component.admin.mapper;

import com.seinfo.seinfomationA.component.admin.model.TeamMangerModel;
import com.seinfo.seinfomationA.component.admin.model.TeamModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ManageTeamMapper {
    void insertTeamMember(TeamMangerModel data);
    void updateTeamMember(TeamMangerModel data);
    Map<String, Object> selectTeamEmailOwner(Map<String, Object> param);
    Map<String, Object> selectLoginIdentity(Map<String, Object> param);
    Map<String, Object> selectLoginIdOwner(Map<String, Object> param);

    List<Map<String, Object>> selectTeamMemberList(Map<String, Object> param);
    List<Map<String, Object>> selectMemberList();
    void updateTeamManger(Map<String, Object> param) throws Exception;
    void deleteTeamManger(@Param("userUid") int userUid) throws Exception;
    List<Map<String, Object>> selectTeamList(Map<String, Object> param);
    void upsertTeam(List<TeamModel> list) throws Exception;
    void deleteTeam(@Param("teamNo") int teamNo) throws Exception;
}
