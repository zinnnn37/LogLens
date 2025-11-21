"""
OpenSearch index naming utility

Generates index names in the new format:
{project_uuid_with_underscores}_{YYYY}_{MM}

Example: 3a73c7d4_8176_3929_b72f_d5b921daae67_2025_11
"""

from datetime import datetime


def format_index_name(project_uuid: str, date: datetime = None) -> str:
    """
    Generate OpenSearch index name in new format.

    Format: {project_uuid_with_underscores}_{YYYY}_{MM}

    Args:
        project_uuid: Project UUID (may contain hyphens)
        date: Optional date for the index (defaults to current date)

    Returns:
        str: Formatted index name

    Examples:
        >>> format_index_name("3a73c7d4-8176-3929-b72f-d5b921daae67")
        "3a73c7d4_8176_3929_b72f_d5b921daae67_2025_11"

        >>> format_index_name("test-project-uuid")
        "test_project_uuid_2025_11"
    """
    if date is None:
        date = datetime.now()

    # Replace hyphens with underscores in UUID
    uuid_formatted = project_uuid.replace('-', '_')

    # Get year and month in YYYY and MM format
    year = date.strftime('%Y')
    month = date.strftime('%m')

    return f"{uuid_formatted}_{year}_{month}"
