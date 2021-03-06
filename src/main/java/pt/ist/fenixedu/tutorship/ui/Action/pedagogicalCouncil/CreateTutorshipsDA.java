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
package pt.ist.fenixedu.tutorship.ui.Action.pedagogicalCouncil;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Shift;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.service.services.exceptions.FenixServiceException;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.fenixedu.bennu.struts.portal.EntryPoint;
import org.fenixedu.bennu.struts.portal.StrutsFunctionality;

import pt.ist.fenixWebFramework.renderers.utils.RenderUtils;
import pt.ist.fenixedu.tutorship.domain.Tutorship;
import pt.ist.fenixedu.tutorship.dto.coordinator.tutor.StudentsByEntryYearBean;
import pt.ist.fenixedu.tutorship.dto.coordinator.tutor.TutorshipErrorBean;
import pt.ist.fenixedu.tutorship.service.coordinator.InsertTutorship;
import pt.ist.fenixedu.tutorship.ui.TutorshipApplications.TutorshipApp;

/**
 * Class CreateTutorshipsDA.java
 *
 * @author jaime created on Aug 3, 2010
 */

@StrutsFunctionality(app = TutorshipApp.class, path = "create-tutorships", titleKey = "link.tutorship.create",
        bundle = "ApplicationResources")
@Mapping(path = "/createTutorships", module = "pedagogicalCouncil")
@Forwards(@Forward(name = "prepareCreate", path = "/pedagogicalCouncil/tutorship/createTutorships.jsp"))
public class CreateTutorshipsDA extends TutorManagementDispatchAction {

    private static int TUTORSHIP_DURATION = 2;

    @EntryPoint
    public ActionForward prepareCreation(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        request.setAttribute("tutorateBean", new ContextTutorshipCreationBean());

        return mapping.findForward("prepareCreate");
    }

    public ActionForward prepareViewCreateTutorship(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        ContextTutorshipCreationBean bean = getRenderedObject("tutorateBean");
        request.setAttribute("tutorateBean", bean);
        if (bean.getShift() != null && bean.getExecutionCourse() != null && bean.getExecutionDegree() != null
                && bean.getExecutionSemester() != null) {
            // get all students from ExecCourse
            List<Person> students = new ArrayList<Person>();

            Shift shift = bean.getShift();
            for (Registration registration : shift.getStudentsSet()) {
                if (validForListing(registration, bean.getExecutionDegree())) {
                    students.add(registration.getPerson());
                }
            }
            RenderUtils.invalidateViewState();
            request.setAttribute("students", students);
            request.setAttribute("tutorBean", new TeacherTutorshipCreationBean(bean.getExecutionDegree()));
            return mapping.findForward("prepareCreate");
        } else {
            RenderUtils.invalidateViewState();
            return mapping.findForward("prepareCreate");
        }
    }

    /**
     * Select people which have registrations in the choosen Degree and have
     * never had a Tutor assigned
     *
     * @param registration
     * @param executionDegree
     * @return
     */
    public boolean validForListing(Registration registration, ExecutionDegree executionDegree) {
        Student student = registration.getStudent();

        if (student.hasActiveRegistrationFor(executionDegree.getDegree())) {
            if (Tutorship.getActiveTutorship(registration.getLastStudentCurricularPlan()) == null) {
                for (StudentCurricularPlan studentCurricularPlan : registration.getStudentCurricularPlansSet()) {
                    if (studentCurricularPlan.getTutorshipsSet().size() == 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public ActionForward prepareStudentsAndTeachers(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        ContextTutorshipCreationBean bean = getRenderedObject("tutorateBean");
        request.setAttribute("tutorateBean", bean);
        return mapping.findForward("prepareCreate");
    }

    public ActionForward createTutorship(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Boolean errorEncountered = false;
        String[] selectedPersons = request.getParameterValues("selectedPersons");
        TeacherTutorshipCreationBean tutorBean = getRenderedObject("tutorBean");
        ContextTutorshipCreationBean contextBean = getRenderedObject("tutorateBean");

        StudentsByEntryYearBean selectedStudentsAndTutorBean =
                new StudentsByEntryYearBean(contextBean.getExecutionSemester().getExecutionYear());
        // Initialize Tutorship creation bean to use in InsertTutorship Service
        BeanInitializer.initializeBean(selectedStudentsAndTutorBean, tutorBean, contextBean, selectedPersons, TUTORSHIP_DURATION);

        List<TutorshipErrorBean> tutorshipsNotInserted = new ArrayList<TutorshipErrorBean>();
        try {
            tutorshipsNotInserted =
                    InsertTutorship.runInsertTutorship(contextBean.getExecutionDegree().getExternalId(),
                            selectedStudentsAndTutorBean);
        } catch (FenixServiceException e) {
            addActionMessage(request, e.getMessage(), e.getArgs());
            errorEncountered = true;
        }
        if (!tutorshipsNotInserted.isEmpty()) {
            errorEncountered = true;
            for (TutorshipErrorBean tutorship : tutorshipsNotInserted) {
                addActionMessage(request, tutorship.getMessage(), tutorship.getArgs());
            }
            if (tutorshipsNotInserted.size() < selectedPersons.length) {
                Integer argument = selectedPersons.length - tutorshipsNotInserted.size();
                String[] messageArgs = { argument.toString() };
                addActionMessage(request, "label.create.tutorship.remaining.correct", messageArgs);
            }
            return mapping.findForward("prepareCreate");
        } else if (!errorEncountered) {
            request.setAttribute("success", "Sucess");
            return prepareCreation(mapping, actionForm, request, response);
        }
        return prepareCreation(mapping, actionForm, request, response);
    }
}
