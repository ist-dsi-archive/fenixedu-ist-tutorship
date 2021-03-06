/**
 * Copyright © 2011 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Tutorship.
 *
 * FenixEdu Tutorship is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Tutorship is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Tutorship.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.tutorship.ui.Action.commons.tutorship;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.curriculum.EnrollmentState;
import org.fenixedu.academic.service.services.exceptions.FenixServiceException;
import org.fenixedu.academic.ui.struts.action.base.FenixDispatchAction;

import pt.ist.fenixedu.tutorship.domain.Tutorship;
import pt.ist.fenixedu.tutorship.dto.teacher.tutor.PerformanceGridTableDTO;
import pt.ist.fenixedu.tutorship.dto.teacher.tutor.PerformanceGridTableDTO.PerformanceGridLine;
import pt.ist.fenixedu.tutorship.dto.teacher.tutor.TutorStatisticsBean;
import pt.ist.fenixedu.tutorship.service.teacher.CreatePerformanceGridTable;

public abstract class StudentsPerformanceGridDispatchAction extends FenixDispatchAction {

    protected PerformanceGridTableDTO createPerformanceGridTable(HttpServletRequest request, List<Tutorship> tutors,
            ExecutionYear entryYear, ExecutionYear monitoringYear) {

        PerformanceGridTableDTO performanceGridTable = null;
        try {
            performanceGridTable = CreatePerformanceGridTable.runCreatePerformanceGridTable(tutors, entryYear, monitoringYear);
        } catch (FenixServiceException ex) {
            addActionMessage(request, ex.getMessage(), ex.getArgs());
        }

        return performanceGridTable;
    }

    /*
     * AUXILIARY METHODS
     */
    protected void getStatisticsAndPutInTheRequest(HttpServletRequest request, PerformanceGridTableDTO performanceGridTable) {
        final List<PerformanceGridLine> performanceGridLines = performanceGridTable.getPerformanceGridTableLines();

        Map<Integer, TutorStatisticsBean> statisticsByApprovedEnrolmentsNumber = new HashMap<Integer, TutorStatisticsBean>();
        int maxApprovedEnrolments = 0;

        for (PerformanceGridLine studentGridLine : performanceGridLines) {
            maxApprovedEnrolments = Math.max(maxApprovedEnrolments, studentGridLine.getApprovedEnrolmentsNumber());

            if (statisticsByApprovedEnrolmentsNumber.containsKey(studentGridLine.getApprovedEnrolmentsNumber())) {
                TutorStatisticsBean tutorStatisticsbean =
                        statisticsByApprovedEnrolmentsNumber.get(studentGridLine.getApprovedEnrolmentsNumber());
                tutorStatisticsbean.setStudentsNumber(tutorStatisticsbean.getStudentsNumber() + 1);
            } else {
                TutorStatisticsBean tutorStatisticsBean =
                        new TutorStatisticsBean(1, studentGridLine.getApprovedEnrolmentsNumber(), performanceGridLines.size());
                tutorStatisticsBean.setApprovedEnrolmentsNumber(studentGridLine.getApprovedEnrolmentsNumber());
                statisticsByApprovedEnrolmentsNumber.put(studentGridLine.getApprovedEnrolmentsNumber(), tutorStatisticsBean);
            }
        }

        putStatisticsInTheRequest(request, maxApprovedEnrolments, performanceGridLines.size(),
                statisticsByApprovedEnrolmentsNumber, "tutorStatistics");
    }

    protected void putAllStudentsStatisticsInTheRequest(HttpServletRequest request, List<StudentCurricularPlan> students,
            ExecutionYear currentMonitoringYear) {
        Map<Integer, TutorStatisticsBean> statisticsByApprovedEnrolmentsNumber = new HashMap<Integer, TutorStatisticsBean>();

        int maxApprovedEnrolments = 0;

        for (StudentCurricularPlan scp : students) {
            List<Enrolment> enrolments = scp.getEnrolmentsByExecutionYear(currentMonitoringYear);
            int approvedEnrolments = 0;

            for (Enrolment enrolment : enrolments) {
                if (!enrolment.getCurricularCourse().isAnual() && enrolment.getEnrollmentState().equals(EnrollmentState.APROVED)) {
                    approvedEnrolments++;
                }
            }

            maxApprovedEnrolments = Math.max(maxApprovedEnrolments, approvedEnrolments);

            if (statisticsByApprovedEnrolmentsNumber.containsKey(approvedEnrolments)) {
                TutorStatisticsBean tutorStatisticsbean = statisticsByApprovedEnrolmentsNumber.get(approvedEnrolments);
                tutorStatisticsbean.setStudentsNumber(tutorStatisticsbean.getStudentsNumber() + 1);
            } else {
                TutorStatisticsBean tutorStatisticsBean = new TutorStatisticsBean(1, approvedEnrolments, students.size());
                tutorStatisticsBean.setApprovedEnrolmentsNumber(approvedEnrolments);
                statisticsByApprovedEnrolmentsNumber.put(approvedEnrolments, tutorStatisticsBean);
            }
        }

        putStatisticsInTheRequest(request, maxApprovedEnrolments, students.size(), statisticsByApprovedEnrolmentsNumber,
                "allStudentsStatistics");
    }

    private void putStatisticsInTheRequest(HttpServletRequest request, Integer maxApprovedEnrolments, Integer studentsSize,
            Map<Integer, TutorStatisticsBean> statisticsByApprovedEnrolmentsNumber, String attributeId) {

        if (studentsSize != 0) {
            List<TutorStatisticsBean> statistics = new ArrayList<TutorStatisticsBean>();
            for (int i = 0; i <= maxApprovedEnrolments; i++) {
                if (statisticsByApprovedEnrolmentsNumber.containsKey(i)) {
                    statistics.add(statisticsByApprovedEnrolmentsNumber.get(i));
                } else {
                    statistics.add(new TutorStatisticsBean(0, i, studentsSize));
                }
            }

            Collections.sort(statistics);
            request.setAttribute(attributeId, statistics);
        }
    }
}
