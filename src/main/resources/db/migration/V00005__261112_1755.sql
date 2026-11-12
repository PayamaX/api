alter table if exists payamak_report add column decided_diagnostics varchar(255) check ((decided_diagnostics in ('Important','Useful','Spam')));
alter table if exists payamak_report add column flagged boolean not null;
alter table if exists payamak_report add column reviewer_comment varchar(255);
alter table if exists payamak_report add column state varchar(255) check ((state in ('PENDING','REVIEWED','POSTPONED')));
