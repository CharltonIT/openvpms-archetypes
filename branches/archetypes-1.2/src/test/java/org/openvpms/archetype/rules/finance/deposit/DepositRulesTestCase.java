/*
 *  Version: 1.0
 *
 *  The contents of this file are subject to the OpenVPMS License Version
 *  1.0 (the 'License'); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *  http://www.openvpms.org/license/
 *
 *  Software distributed under the License is distributed on an 'AS IS' basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the
 *  License.
 *
 *  Copyright 2006 (C) OpenVPMS Ltd. All Rights Reserved.
 *
 *  $Id$
 */

package org.openvpms.archetype.rules.finance.deposit;

import static org.openvpms.archetype.rules.finance.deposit.DepositRuleException.ErrorCode.InvalidDepositArchetype;
import static org.openvpms.archetype.rules.finance.deposit.DepositRuleException.ErrorCode.UndepositedDepositExists;
import org.openvpms.archetype.test.ArchetypeServiceTest;
import org.openvpms.component.business.domain.im.act.Act;
import org.openvpms.component.business.domain.im.act.FinancialAct;
import org.openvpms.component.business.domain.im.party.Party;
import org.openvpms.component.business.service.archetype.ArchetypeServiceHelper;
import org.openvpms.component.business.service.archetype.IArchetypeService;
import org.openvpms.component.business.service.archetype.helper.ActBean;
import org.openvpms.component.business.service.archetype.helper.IMObjectBean;
import org.openvpms.component.business.service.ruleengine.RuleEngineException;

import java.util.Date;


/**
 * Tests the {@link DepositRules} class.
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate: 2006-05-02 05:16:31Z $
 */
public class DepositRulesTestCase extends ArchetypeServiceTest {

    /**
     * The account.
     */
    private Party account;


    /**
     * Verifies that an <em>act.bankDeposit</em> with 'UnDeposited' status
     * can only be saved if there are no other uncleared bank deposits for
     * the same account.<br/>
     * Requires the rule <em>archetypeService.save.act.bankDeposit.before</em>.
     */
    public void testSaveUndepositedDeposit() {
        ActBean deposit1 = createDeposit(DepositStatus.UNDEPOSITED);
        deposit1.save();

        // can save the same deposit multiple times
        deposit1.save();

        ActBean deposit2 = createDeposit(DepositStatus.UNDEPOSITED);
        try {
            deposit2.save();
            fail("Expected save of second undeposited bank deposit to fail");
        } catch (RuleEngineException expected) {
            Throwable cause = expected.getCause();
            while (cause != null && !(cause instanceof DepositRuleException)) {
                cause = cause.getCause();
            }
            assertTrue(cause instanceof DepositRuleException);
            DepositRuleException exception = (DepositRuleException) cause;
            assertEquals(UndepositedDepositExists, exception.getErrorCode());
        }
    }

    /**
     * Verifies that multiple <em>act.bankDeposit</em> with 'Deposited' status
     * canbe saved for the same deposit account.<br/>
     * Requires the rule <em>archetypeService.save.act.bankDeposit.before</em>.
     */
    public void testSaveDepositedDeposit() {
        for (int i = 0; i < 3; ++i) {
            ActBean deposit = createDeposit(DepositStatus.DEPOSITED);
            deposit.save();
        }
    }

    /**
     * Verifies that {@link DepositRules#checkCanSaveBankDeposit} throws
     * DepositRuleException if invoked for an invalid act.
     */
    public void testCheckCanSaveBankDepositWithInvalidAct() {
        IArchetypeService service
                = ArchetypeServiceHelper.getArchetypeService();
        ActBean bean = createAct("act.tillBalance");
        FinancialAct act = (FinancialAct) bean.getAct();
        try {
            DepositRules.checkCanSaveBankDeposit(act, service);
        } catch (DepositRuleException expected) {
            assertEquals(InvalidDepositArchetype, expected.getErrorCode());
        }
    }

    /**
     * Tests the {@link DepositRules#deposit} method.
     */
    public void testDeposit() {
        IArchetypeService service
                = ArchetypeServiceHelper.getArchetypeService();
        ActBean deposit = createDeposit(DepositStatus.UNDEPOSITED);
        DepositRules.deposit(deposit.getAct(), service);

        // reload the account to pick up the updates
        account = (Party) get(account.getObjectReference());
        assertNotNull(account);

        IMObjectBean bean = new IMObjectBean(account);
        Date lastDeposit = bean.getDate("lastDeposit");
        Date now = new Date();

        assertTrue(now.compareTo(lastDeposit) == 1); // expect now > lastDeposit

    }

    /**
     * Sets up the test case.
     *
     * @throws Exception for any error
     */
    @Override
    protected void onSetUp() throws Exception {
        super.onSetUp();
        account = DepositTestHelper.createDepositAccount();
    }

    /**
     * Helper to create an <em>act.bankDepsit</em> wrapped in a bean.
     *
     * @param status the act status
     * @return a new act
     */
    private ActBean createDeposit(String status) {
        ActBean act = createAct("act.bankDeposit");
        act.setStatus(status);
        act.setParticipant("participation.deposit", account);
        return act;
    }

    /**
     * Helper to create a new act, wrapped in a bean.
     *
     * @param shortName the act short name
     * @return a new act wrapped in a bean
     */
    private ActBean createAct(String shortName) {
        Act act = (Act) create(shortName);
        assertNotNull(act);
        return new ActBean(act);
    }


}
