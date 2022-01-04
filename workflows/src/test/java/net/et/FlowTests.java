package net.et;

import net.et.flows.LoanFlow;
import net.et.states.LoanState;
import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.TransactionVerificationException;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;

    @Before
    public void setup() {

        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("net.et.contracts"),
                TestCordapp.findCordapp("net.et.flows"))));
        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }


    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void flowRejectsInvalidLoans() throws Exception {
        // The LoanContract specifies that loans cannot have negative values.
        LoanFlow.Initiator flow = new LoanFlow.Initiator(-1, b.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();

        // The LoanContract specifies that loans cannot have negative values.
        exception.expectCause(instanceOf(TransactionVerificationException.class));
        future.get();
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheInitiator() throws Exception {
        LoanFlow.Initiator flow = new LoanFlow.Initiator(1, b.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(b.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheAcceptor() throws Exception {
        LoanFlow.Initiator flow = new LoanFlow.Initiator(1, b.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(a.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void flowRecordsATransactionInBothPartiesTransactionStorages() throws Exception {
        LoanFlow.Initiator flow = new LoanFlow.Initiator(1, b.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : ImmutableList.of(a, b)) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }

    @Test
    public void recordedTransactionHasNoInputsAndASingleOutputTheInputLoan() throws Exception {
        double loanValue = 1;
        LoanFlow.Initiator flow = new LoanFlow.Initiator(loanValue, b.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : ImmutableList.of(a, b)) {
            SignedTransaction recordedTx = node.getServices().getValidatedTransactions().getTransaction(signedTx.getId());
            List<TransactionState<ContractState>> txOutputs = recordedTx.getTx().getOutputs();
            assert (txOutputs.size() == 1);

            LoanState recordedState = (LoanState) txOutputs.get(0).getData();
            assertEquals(recordedState.getValue(), loanValue);
            assertEquals(recordedState.getLender(), a.getInfo().getLegalIdentities().get(0));
            assertEquals(recordedState.getBorrower(), b.getInfo().getLegalIdentities().get(0));
        }
    }

    @Test
    public void flowRecordsTheCorrectLoanInBothPartiesVaults() throws Exception {
        double loanValue = 1;
        LoanFlow.Initiator flow = new LoanFlow.Initiator(1, b.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        future.get();

        // We check the recorded loan in both vaults.
        for (StartedMockNode node : ImmutableList.of(a, b)) {
            node.transaction(() -> {
                List<StateAndRef<LoanState>> loans = node.getServices().getVaultService().queryBy(LoanState.class).getStates();
                assertEquals(1, loans.size());
                LoanState recordedState = loans.get(0).getState().getData();
                assertEquals(recordedState.getValue(), loanValue);
                assertEquals(recordedState.getLender(), a.getInfo().getLegalIdentities().get(0));
                assertEquals(recordedState.getBorrower(), b.getInfo().getLegalIdentities().get(0));
                return null;
            });
        }
    }
}