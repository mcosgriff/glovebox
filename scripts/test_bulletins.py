# /// script
# requires-python = ">=3.11"
# dependencies = ["pytest==8.4.2"]
# ///
"""Tests for bulletins.py. Run: uv run scripts/test_bulletins.py"""

import sys

import pytest

import bulletins as b


def row(id="11000001", added="20250301", number="26TD01", mfr="20250210", type="Service Campaign",
        make="TOYOTA", model="4RUNNER", year="2025", components="STRUCTURE", summary="Made-up summary."):
    """One line of NHTSA's flat file, fields in its order (TSBS.txt)."""
    return "\t".join([id, "", added, number, mfr, "", type, make, model, year, components, "", "", summary])


COVERED = [b.Covered("Toyota", "4Runner", range(2010, 2027), ["4RUNNER", "4RUNNER I-FORCE MAX HYBRID"], [])]


def test_nhtsa_types_for_free_repairs_are_kept_as_typed():
    assert b.kind("Service Campaign", "x") == ("SERVICE_CAMPAIGN", False)
    assert b.kind("Warranty Program/Extension", "x") == ("WARRANTY_EXTENSION", False)
    assert b.kind("Over The Air", "x") == ("OVER_THE_AIR", False)


def test_other_types_are_left_out():
    assert b.kind("Other", "Toyota is announcing a Warranty Enhancement Program") is None
    assert b.kind("Emissions", "x") is None
    assert b.kind("Service Bulletin/Repair Instructions", "Engine knocks on cold start.") is None


def test_an_untyped_bulletin_is_kept_only_when_its_summary_names_a_free_repair_program():
    assert b.kind("Service Bulletin/Repair Instructions", "Toyota is announcing a Warranty Enhancement Program") == ("WARRANTY_EXTENSION", True)
    assert b.kind("Service Bulletin/Repair Instructions", "Customer Support Program ZHB covers the bulbs") == ("WARRANTY_EXTENSION", True)
    assert b.kind("", "TECHNICAL INSTRUCTIONS FOR SPECIAL SERVICE CAMPAIGN J0N") == ("SERVICE_CAMPAIGN", True)
    assert b.kind("Service Bulletin/Repair Instructions", "LIMITED SERVICE CAMPAIGN - 50G - VECI label") == ("SERVICE_CAMPAIGN", True)


def test_rows_of_one_bulletin_become_one_entry_with_every_covered_model_year():
    lines = [
        row(year="2024"), row(year="2025"), row(year="2025", components="ELECTRICAL"),
        row(model="4RUNNER I-FORCE MAX HYBRID", year="2025"),
        row(model="TACOMA", year="2025"),  # not covered
        row(year="2009"),  # before the covered years
    ]
    out = b.extract_from(lines, COVERED).bulletins
    entry = out["Toyota/4Runner"][0]
    assert entry["id"] == "11000001"
    assert entry["kind"] == "SERVICE_CAMPAIGN" and entry["byWording"] is False
    assert entry["products"] == [{"model": "4RUNNER", "years": [2024, 2025]}, {"model": "4RUNNER I-FORCE MAX HYBRID", "years": [2025]}]
    assert entry["components"] == ["ELECTRICAL", "STRUCTURE"]
    assert entry["number"] == "26TD01" and entry["date"] == "2025-02-10"


def test_makes_are_matched_ignoring_case_and_rows_short_of_fields_are_skipped():
    out = b.extract_from([row(make="Toyota"), "11000002\tbroken"], COVERED).bulletins
    assert [e["id"] for e in out["Toyota/4Runner"]] == ["11000001"]


def test_bulletins_are_newest_first():
    out = b.extract_from([row(id=i, mfr=d, summary=f"Bulletin {i}.") for i, d in (("1", "20200101"), ("2", "20240101"), ("3", ""))], COVERED).bulletins
    assert [e["id"] for e in out["Toyota/4Runner"]] == ["2", "1", "3"]


def test_the_pdf_link_is_the_first_candidate_nhtsa_answers_and_none_when_it_answers_none():
    asked = []

    def head(url):
        asked.append(url)
        return url.endswith("/2014/MC-10109359-9999.pdf")

    assert b.pdf_link("10109359", "20170801", "20141107", head) == "https://static.nhtsa.gov/odi/tsbs/2014/MC-10109359-9999.pdf"
    assert asked[0] == "https://static.nhtsa.gov/odi/tsbs/2017/MC-10109359-0001.pdf"
    assert b.pdf_link("10062057", "20160412", "20130827", lambda url: False) is None


def test_a_covered_model_without_nhtsa_names_is_an_error_so_adding_a_vehicle_cant_skip_it():
    schedules = [{"make": "Toyota", "model": "4Runner", "years": {"from": 2010, "to": 2026}},
                 {"make": "Toyota", "model": "Crown", "years": {"from": 2023, "to": 2026}}]
    try:
        b.covered(schedules, {"Toyota/4Runner": ["4RUNNER"]})
        assert False, "should have failed"
    except SystemExit as e:
        assert "Toyota/Crown" in str(e)


def test_covered_years_and_aliases_are_the_union_of_a_models_schedules():
    schedules = [{"make": "Toyota", "model": "4Runner", "years": {"from": 2010, "to": 2012}},
                 {"make": "Toyota", "model": "4Runner", "years": {"from": 2020, "to": 2020}, "modelAliases": ["4Runner Hybrid"]}]
    (c,) = b.covered(schedules, {"Toyota/4Runner": ["4RUNNER"]})
    assert list(c.years) == [2010, 2011, 2012, 2020]
    assert c.aliases == ["4Runner Hybrid"]


def test_an_exact_repeat_of_a_bulletin_under_another_nhtsa_id_is_dropped():
    lines = [row(id="1", number="K0P", summary="Same text."), row(id="2", number="K0P", summary="Same  text."),
             row(id="3", number="K0P", summary="Other text."), row(id="4", number="19TE12", summary="Same text.")]
    assert [e["id"] for e in b.extract_from(lines, COVERED).bulletins["Toyota/4Runner"]] == ["4", "3", "2"]


def test_a_line_is_read_as_utf8_and_one_that_isnt_stops_the_script():
    assert b.decode("“Program” – 2019".encode("utf-8"), "f.zip", 1) == "“Program” – 2019"
    with pytest.raises(SystemExit, match="f.zip line 7"):
        b.decode("“Program”".encode("cp1252"), "f.zip", 7)


def test_a_summary_naming_both_a_service_campaign_and_a_support_program_is_a_campaign():
    summary = "TECHNICAL INSTRUCTIONS FOR LIMITED SERVICE CAMPAIGN K0P CUSTOMER SUPPORT PROGRAM 19TE12"
    assert b.kind("Service Bulletin/Repair Instructions", summary) == ("SERVICE_CAMPAIGN", True)


def test_the_list_is_dated_by_the_newest_line_nhtsa_added_to_its_files():
    assert b.extract_from([row(added="20240101"), row(added="20261024", model="TACOMA"), "x\tx\tnot-a-date"], COVERED).as_of == "2026-10-24"


def test_a_summary_is_one_line_with_no_control_characters():
    out = b.extract_from([row(summary="  Line one\x0b\x1a  and\u2028two  ")], COVERED).bulletins
    assert out["Toyota/4Runner"][0]["summary"] == "Line one and two"


def test_a_models_file_names_the_years_it_covers_so_the_app_knows_what_isnt():
    (c,) = b.covered([{"make": "Toyota", "model": "4Runner", "years": {"from": 2019, "to": 2020}, "modelAliases": ["4Runner TRD"]}], {"Toyota/4Runner": ["4RUNNER"]})
    doc = b.document(c, [], "2026-10-24")
    assert doc == {"make": "Toyota", "model": "4Runner", "modelAliases": ["4Runner TRD"], "years": [2019, 2020], "asOf": "2026-10-24", "bulletins": []}


if __name__ == "__main__":
    sys.exit(pytest.main([__file__, "-q"]))
