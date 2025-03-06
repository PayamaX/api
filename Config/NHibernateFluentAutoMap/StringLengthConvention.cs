using System.ComponentModel.DataAnnotations;
using FluentNHibernate.Conventions;
using FluentNHibernate.Conventions.Instances;

namespace PayamaX.Portal.Config.NHibernateFluentAutoMap;

/// <summary>
/// 
/// </summary>
public class StringLengthConvention : AttributePropertyConvention<StringLengthAttribute>
{
    /// <summary>
    /// 
    /// </summary>
    /// <param name="attribute"></param>
    /// <param name="instance"></param>
    protected override void Apply(StringLengthAttribute attribute, IPropertyInstance instance)
    {
        ArgumentNullException.ThrowIfNull(attribute);
        ArgumentNullException.ThrowIfNull(instance);
        instance.Length(attribute.MaximumLength);
    }
}