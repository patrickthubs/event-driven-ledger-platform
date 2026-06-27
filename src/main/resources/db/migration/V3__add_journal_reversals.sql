alter table journal_entry
    add column reversal_of_journal_entry_id uuid;

alter table journal_entry
    add constraint fk_journal_entry_reversal
        foreign key (reversal_of_journal_entry_id) references journal_entry(id);

alter table journal_entry
    add column reversal_reason varchar(200);

alter table journal_entry
    add constraint uq_journal_entry_reversal_of_journal_entry
        unique (reversal_of_journal_entry_id);
