using System.Diagnostics.CodeAnalysis;

namespace PayamaX.Portal.Utility;

public static class StringUtility
{
    public static bool IsUsable([NotNullWhen(true)]this string? text)
    {
        return !string.IsNullOrWhiteSpace(text);
    }
}