alter table if exists payamak_report add column detected_diagnostics varchar(255) check ((detected_diagnostics in ('Important','Useful','Spam')));
alter table if exists payamak_report add column expected_diagnostics varchar(255) check ((expected_diagnostics in ('Important','Useful','Spam')));
alter table if exists payamak_report add column reporter_user_global_id uuid;
create table payamak_rule_process_result_report (id bigint not null, general_rank float4 not null, global_id uuid, position integer not null, processor_name varchar(255), processor_provided_comment varchar(255), processor_version varchar(255), user_values_rank float4 not null, report_id bigint, primary key (id));
alter table if exists payamak_rule_process_result_report drop constraint if exists uq__payamak_rule_process_result_report__global_id;
alter table if exists payamak_rule_process_result_report add constraint uq__payamak_rule_process_result_report__global_id unique (global_id);
alter table if exists ranked_origin drop constraint if exists uq__ranked_origin__origin;
alter table if exists ranked_origin add constraint uq__ranked_origin__origin unique (origin);
alter table if exists payamak_rule_process_result_report add constraint fk__payamak_rule_process_result_report__report_id foreign key (report_id) references payamak_report;
