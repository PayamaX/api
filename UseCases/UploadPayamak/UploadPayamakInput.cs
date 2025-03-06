using System.Collections.ObjectModel;

namespace PayamaX.Portal.UseCases.UploadPayamak;

/// <summary>
/// 
/// </summary>
/// <param name="PayamakProcessResults"></param>
public record UploadPayamakInput(ReadOnlyCollection<PayamakExpectedProcessResultPortable> PayamakProcessResults);