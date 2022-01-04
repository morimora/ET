package net.et.schema;

import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Arrays;
import java.util.UUID;
//4.6 changes
import org.hibernate.annotations.Type;
import javax.annotation.Nullable;

/**
 * An LoanState schema.
 */
public class LoanSchemaV1 extends MappedSchema {
    public LoanSchemaV1() {
        super(LoanSchema.class, 1, Arrays.asList(PersistentLoan.class));
    }

    @Nullable
    @Override
    public String getMigrationResource() {
        return "loan.changelog-master";
    }

    @Entity
    @Table(name = "loan_states")
    public static class PersistentLoan extends PersistentState {
        @Column(name = "lender") private final String lender;
        @Column(name = "borrower") private final String borrower;
        @Column(name = "value") private final double value;
        @Column(name = "linear_id") @Type (type = "uuid-char") private final UUID linearId;


        public PersistentLoan(String lender, String borrower, double value, UUID linearId) {
            this.lender = lender;
            this.borrower = borrower;
            this.value = value;
            this.linearId = linearId;
        }

        // Default constructor required by hibernate.
        public PersistentLoan() {
            this.lender = null;
            this.borrower = null;
            this.value = 0;
            this.linearId = null;
        }

        public String getLender() {
            return lender;
        }

        public String getBorrower() {
            return borrower;
        }

        public double getValue() {
            return value;
        }

        public UUID getId() {
            return linearId;
        }
    }
}