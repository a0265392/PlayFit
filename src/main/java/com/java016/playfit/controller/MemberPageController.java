package com.java016.playfit.controller;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.java016.playfit.model.Avatar;
import com.java016.playfit.model.DailyRecord;
import com.java016.playfit.model.FitAchieve;
import com.java016.playfit.model.FitActivity;
import com.java016.playfit.model.HealthRecord;
import com.java016.playfit.model.PersonalGoal;
import com.java016.playfit.model.User;
import com.java016.playfit.service.DailyRecordService;
import com.java016.playfit.service.HealthRecordService;
import com.java016.playfit.service.MemberService;
import com.java016.playfit.service.PersonalGoalService;
import com.java016.playfit.service.UserService;

@Controller
public class MemberPageController {

	MemberService memberService;

	UserService userService;

	HealthRecordService healthRecordService;

	PersonalGoalService personalGoalService;

	DailyRecordService dailyRecordService;

	@Autowired
	public MemberPageController(UserService userService, HealthRecordService healthRecordService,
			PersonalGoalService personalGoalService, DailyRecordService dailyRecordService,
			MemberService memberService) {
		this.userService = userService;
		this.healthRecordService = healthRecordService;
		this.personalGoalService = personalGoalService;
		this.dailyRecordService = dailyRecordService;
		this.memberService = memberService;
	}

	// ????????????
	@RequestMapping("/MemberPage") // ???????????????GET???POST(forward:/)
	public String showMemberPage(Model model, RedirectAttributes ra,
								 HttpServletRequest request, HttpSession session) {
		session.setAttribute("userId", userService.getLoginUser());
		// ??????????????? ??????
//		// ????????????????????????
//		boolean isEnable = userService.isLoginUserEnable();
//		
//		// ????????????????????????
//		if (!isEnable) {
//			return "redirect:/certificationEmail";
//		}
		
		// ??????????????? + Id
		int userId = userService.getLoginUserId();
		User user = userService.getUserById(userId);
		model.addAttribute("user", user);

		// ??????????????????
		Avatar avatar = user.getAvatar();
		model.addAttribute("avatar", avatar);

		// ????????????????????????
		String today = memberService.getFormatMemberPageDate();
		model.addAttribute("today", today);

		// ?????????????????????
		java.util.Date utilDate = new java.util.Date();
		// ???????????????SQL?????????Date
		java.sql.Date sqlDate = new java.sql.Date(utilDate.getTime());

		// ??????????????????????????????
		DailyRecord todayRecord = dailyRecordService.findByUserIdAndDate(userId, sqlDate);
		model.addAttribute("todayRecord", todayRecord);

		// ???????????????????????????
		if (todayRecord == null) {
			model.addAttribute("activityStatus", false);
			model.addAttribute("calLost", 0);
			model.addAttribute("calGain", 0);
		} else {

			// ?????????????????? & ????????????
			LinkedHashMap<FitAchieve, FitActivity> activityStatus = 
					memberService.getTodayAchieveAndActivity(todayRecord);
			model.addAttribute("activityStatus", activityStatus);
			
			// ????????????&??????
			Integer kcalBurned = todayRecord.getKcalBurned();
			Integer kcalIntake = todayRecord.getKcalIntake();
			
			model.addAttribute("calLost", kcalBurned);
			
			model.addAttribute("calGain", kcalIntake);

		}

		// ?????????????????????
		HealthRecord healthRecord = healthRecordService.findLastDateByUserId(userId);
		model.addAttribute("healthRecord", healthRecord);

		// ?????????????????????
		PersonalGoal personalGoal = personalGoalService.findLastDateByUserId(userId);
		model.addAttribute("personalGoal", personalGoal);

		// ?????? forward:/MemberPage (edit personalGoal)
		String result = (String) request.getAttribute("result");
		if (result != null) {
			if (result.equalsIgnoreCase("error")) {
				ra.addFlashAttribute("updateResult", "error"); // Flash forward ?????????
			}
			if (result.equalsIgnoreCase("success")) {
				ra.addFlashAttribute("updateResult", "success");
			}
			return "redirect:/MemberPage";
		}

		return "MemberPage";
	}

	// ?????????????????????
	@GetMapping(value = "/weeklyExerciseData", 
			produces = { "application/json" })
	@ResponseBody
//	@PreAuthorize("hasRole('PRIME')")
	public Map<Integer, String[]> weeklyExerciseData() {
		Map<Integer, String[]> data = null;
		int userId = userService.getLoginUserId();
		User user = userService.getUserById(userId);
		data = memberService.getWeekExerciseData(user);
		return data;
	}
	
	// ?????????????????????
	@PostMapping(value = "/taskCompletionRate",
			consumes = { "application/json" },
			produces = { "application/json" })
	@ResponseBody
	public Map<String, Double> taskCompletionRate(@RequestBody java.util.Date date) {
		
		java.sql.Date sqlDate = new java.sql.Date(date.getTime());
		
//		System.out.println(sqlDate + "---------------------------------");
		
		int userId = userService.getLoginUserId();
		DailyRecord todayRecord = dailyRecordService.findByUserIdAndDate(userId, sqlDate);
		
		Double completionRate = 0.0 ;
		
		if (todayRecord != null) {
			completionRate = memberService.getTaskCompletionRate(todayRecord);
		}
		
		Map<String, Double> map = new HashMap<>();
		map.put("completionRate", completionRate);
		
		return map;
	}

	// ??????????????????
	@PostMapping("/editPersonGoal")
	public String editPersonGoal(
			@RequestParam("editWeight") Double newGoal, HttpServletRequest request) {

		// user id
		int userId = userService.getLoginUserId();
		User user = userService.getUserById(userId);

		// ?????????????????????
		HealthRecord healthRecord = healthRecordService.findLastDateByUserId(userId);

		// ?????????????????????????????????
		if (newGoal >= healthRecord.getWeight()) {
			request.setAttribute("result", "error"); // ???????????????????????????
			return "forward:/MemberPage"; // forward ?????? = POST Mehod
		}

		// ????????????????????????
		personalGoalService.updatePersonalGoal(newGoal, user, healthRecord);
		request.setAttribute("result", "success"); // ???????????????????????????

		return "forward:/MemberPage"; // forward ?????? = POST Mehod
	}

	// ????????????????????? Modal
	@ModelAttribute("actionToEditForm")
	public String goEditForm() {

		return "editUser";
	}
}









