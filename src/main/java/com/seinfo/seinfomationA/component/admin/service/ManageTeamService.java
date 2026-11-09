package com.seinfo.seinfomationA.component.admin.service;

import com.seinfo.seinfomationA.component.admin.model.TeamMangerModel;
import com.seinfo.seinfomationA.component.admin.model.TeamModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;

public interface ManageTeamService {

    ModelAndView loadTeamManagePage();

    ResponseEntity<?> reloadTeamManagerList(Map<String, Object> param);

    ResponseEntity<?> deleteTeamManger(List<TeamMangerModel> list) throws Exception;

    ResponseEntity<?> upsertTeamManager(List<TeamMangerModel> list) throws Exception;

    ModelAndView loadTeamListPage();

    ResponseEntity<?> reloadTeamList(Map<String, Object> param);

    ResponseEntity<?> upsertTeam(List<TeamModel> list) throws Exception;

    ResponseEntity<?> deleteTeam(List<TeamModel> list) throws Exception;
}
