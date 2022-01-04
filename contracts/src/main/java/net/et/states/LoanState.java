package net.et.states;

import net.et.contracts.LoanContract;
import net.et.schema.LoanSchemaV1;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;

import java.util.Arrays;
import java.util.List;

/**
 * The states object recording loan agreements between two parties.
 *
 * A states must implement [ContractState] or one of its descendants.
 */
@BelongsToContract(LoanContract.class)
public class LoanState implements LinearState, QueryableState {
    private final double value;
    private final Party lender;
    private final Party borrower;
    private final UniqueIdentifier linearId;

    /**
     * @param value the value of the loan.
     * @param lender the party issuing the loan.
     * @param borrower the party receiving and approving the loan.
     */
    public LoanState(double value,
                    Party lender,
                    Party borrower,
                    UniqueIdentifier linearId)
    {
        this.value = value;
        this.lender = lender;
        this.borrower = borrower;
        this.linearId = linearId;
    }

    public double getValue() { return value; }
    public Party getLender() { return lender; }
    public Party getBorrower() { return borrower; }
    @Override public UniqueIdentifier getLinearId() { return linearId; }
    @Override public List<AbstractParty> getParticipants() {
        return Arrays.asList(lender, borrower);
    }

    @Override public PersistentState generateMappedObject(MappedSchema schema) {
        if (schema instanceof LoanSchemaV1) {
            return new LoanSchemaV1.PersistentLoan(
                    this.lender.getName().toString(),
                    this.borrower.getName().toString(),
                    this.value,
                    this.linearId.getId());
        } else {
            throw new IllegalArgumentException("Unrecognised schema $schema");
        }
    }

    @Override public Iterable<MappedSchema> supportedSchemas() {
        return Arrays.asList(new LoanSchemaV1());
    }

    @Override
    public String toString() {
        return String.format("LoanState(value=%s, lender=%s, borrower=%s, linearId=%s)", value, lender, borrower, linearId);
    }
}