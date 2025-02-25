namespace PayamaX.Portal.Utility;

public static class StringUtility
{
    public static bool IsUsable(this string? text)
    {
        return !string.IsNullOrWhiteSpace(text);
    }
}