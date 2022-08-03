[#-- Transform many singular English words to their plural English form.
Rules cribbed from http://kuwamoto.org/2007/12/17/improved-pluralizing-in-php-actionscript-and-ror/ which were cribbed from https://github.com/rails/rails/blob/9e0b3fc7cfba43af55377488f991348e2de24515/activesupport/lib/active_support/inflections.rb , so MIT License at http://b.mit-license.org/ applies --]

[#assign plurals = {
'(quiz)$': '$1zes',
'^(ox)$': '$1en',
'([m|l])ouse$': '$1ice',
'(matr|vert|ind)ix|ex$': '$1ices',
'(x|ch|ss|sh)$': '$1es',
'([^aeiouy]|qu)y$': '$1ies',
'(hive)$': '$1s',
'(?:([^f])fe|([lr])f)$': '$1$2ves',
'(shea|lea|loa|thie)f$': '$1ves',
'sis$': 'ses',
'([ti])um$': '$1a',
'(tomat|potat|ech|her|vet)o$': '$1oes',
'(bu)s$': '$1ses',
'(alias)$': '$1es',
'(octop)us$': '$1i',
'(ax|test)is$': '$1es',
'(us)$': '$1es',
's$': 's',
'$': 's'
}/]

[#assign irregular = {
'move': 'moves',
'foot': 'feet',
'goose': 'geese',
'sex': 'sexes',
'child': 'children',
'man': 'men',
'tooth': 'teeth',
'person': 'people'
}/]

[#assign uncountable = [
'sheep',
'fish',
'deer',
'series',
'species',
'money',
'rice',
'information',
'equipment'
]/]

[#--
    Transform a singular word into its pluralized form. "count" is optional-y; if it's not included it's assumed string should be pluralized, if it's include string should be pluralized only if count != 1.
--]
[#function pluralize string count=false]
    [#if count?is_boolean || count != 1]
        [#if uncountable?seq_contains(string?lower_case)]
            [#return string]
        [/#if]

        [#list irregular?keys as query]
            [#if string?ends_with(query)]
                [#local strings = string?replace(query, irregular[query])/]
                [#return strings]
            [/#if]
        [/#list]

        [#list plurals?keys as query]
            [#local matches = string?matches(query,'i')/]
            [#if matches?size gt 0]
                [#local strings = string?replace(query, plurals[query], 'ir')/]
                [#return strings]
            [/#if]
        [/#list]
    [/#if]
    [#return string]
[/#function]