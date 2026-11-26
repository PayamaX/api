alter table "public"."payamak_report" drop constraint "uq__payamak_report___global_id";
alter table "public"."payamak_report" drop column "desired_diagnostics";
alter table "public"."payamak_report" drop column "hash";
alter table "public"."payamak_report" drop column "repeated_count";
alter table "public"."payamak_report" drop column "reporter";
drop schema "audit" cascade;
alter table "public"."payamak_report"
	alter column "global_id" set not null;
alter table "public"."payamak_report"
	add column "state" varchar(255) not null check (state in ('PENDING', 'REVIEWED', 'POSTPONED'));
alter table "public"."payamak_report"
	alter column "origin" set data type varchar(16);
alter table "public"."payamak_report"
	alter column "origin" set not null;
alter table "public"."payamak_report"
	alter column "body" set data type varchar(4096);
alter table "public"."payamak_report"
	add column "sha512" varchar(128);
alter table "public"."payamak_report"
	add column "reporter_user_global_id" uuid;
alter table "public"."payamak_report"
	alter column "reporter_comment" set data type varchar(4096);
alter table "public"."payamak_report"
	add column "reviewer_comment" varchar(4096);
alter table "public"."payamak_report"
	add column "flagged" boolean not null;
alter table "public"."payamak_report"
	add column "detected_diagnostics" varchar(255) not null check (detected_diagnostics in ('Important', 'Useful', 'Unknown', 'Spam'));
alter table "public"."payamak_report"
	add column "expected_diagnostics" varchar(255) check (expected_diagnostics in ('Important', 'Useful', 'Unknown', 'Spam'));
alter table "public"."payamak_report"
	add constraint "uq__payamak_report__global_id" unique ("global_id");
alter table "public"."payamak_report"
	add check (decided_diagnostics in ('Important', 'Useful', 'Unknown', 'Spam'));
create index "ix__payamak_report__state" on "public"."payamak_report" ("state");
create index "ix__payamak_report__origin" on "public"."payamak_report" ("origin");
create index "ix__payamak_report__flagged" on "public"."payamak_report" ("flagged");
create table "public"."payamak_report_rule_process_result"
(
	"id"                         bigint default nextval('id') not null,
	"global_id"                  uuid                         not null,
	"payamak_report_id"          bigint                       not null,
	"processor_name"             varchar(32)                  not null,
	"processor_version"          integer                      not null,
	"general_rank"               float4                       not null,
	"user_override_rank"         float4,
	"processor_provided_comment" varchar(4096),
	"position"                   integer                      not null,
	primary key ("id")
);
alter table "public"."payamak_report_rule_process_result"
	add constraint "uq__payamak_report_rule_process_result__global_id" unique ("global_id");
alter table "public"."payamak_report_rule_process_result"
	add constraint "fk__payamak_report_rule_process_result__payamak_report_id" foreign key ("payamak_report_id") references "public"."payamak_report" ("id");
create index "ix__payamak_report_rule_process_result__payamak_report_id" on "public"."payamak_report_rule_process_result" ("payamak_report_id");
alter table "public"."ranked_origin"
	alter column "origin" set data type varchar(16);
alter table "public"."ranked_origin"
	alter column "rank" set data type float4;
alter table "public"."ranked_origin"
	alter column "comment" set data type varchar(512);
alter table "public"."ranked_origin"
	add constraint "uq__ranked_origin__origin" unique ("origin");
