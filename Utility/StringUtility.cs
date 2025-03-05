using System.Diagnostics.CodeAnalysis;

namespace PayamaX.Portal.Utility;

/// <summary>
/// 
/// </summary>
public static class StringUtility
{
    /// <summary>
    /// 
    /// </summary>
    /// <param name="text"></param>
    /// <returns></returns>
    public static bool IsUsable([NotNullWhen(true)]this string? text)
    {
        return !string.IsNullOrWhiteSpace(text);
    }
}