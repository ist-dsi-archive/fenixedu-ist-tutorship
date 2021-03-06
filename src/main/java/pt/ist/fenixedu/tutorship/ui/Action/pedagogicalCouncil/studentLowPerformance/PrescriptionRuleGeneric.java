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
package pt.ist.fenixedu.tutorship.ui.Action.pedagogicalCouncil.studentLowPerformance;

import java.math.BigDecimal;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;

import pt.ist.fenixedu.tutorship.domain.PrescriptionEnum;

class PrescriptionRuleGeneric extends AbstractPrescriptionRule {

    public PrescriptionRuleGeneric() {
    }

    @Override
    public boolean isPrescript(Registration registration, BigDecimal ects, int numberOfEntriesStudentInSecretary,
            ExecutionYear executionYear) {
        return ects.compareTo(getMinimumEcts()) < 0
                && numberOfEntriesStudentInSecretary == getNumberOfEntriesStudentInSecretary();
    }

    @Override
    public BigDecimal getMinimumEcts() {
        return new BigDecimal(119.5);
    }

    @Override
    protected int getNumberOfEntriesStudentInSecretary() {
        return 4;
    }

    @Override
    public PrescriptionEnum getPrescriptionEnum() {
        return PrescriptionEnum.ALLPRESCRIPTION;
    }

    @Override
    public ExecutionYear getRegistrationStart(ExecutionYear executionYear) {
        return executionYear.getPreviousExecutionYear().getPreviousExecutionYear().getPreviousExecutionYear();
    }

}
