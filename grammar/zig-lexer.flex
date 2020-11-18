package org.ziglang;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;
import org.ziglang.psi.ZigTypes;
import org.ziglang.ZigTokenType;

%%

%{
  public ZigLexer() { this((java.io.Reader) null); }
%}

%class ZigLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{ return;
%eof}

OTHERWISE=[^]

HEX=[0-9a-fA-F]
CHAR_ESCAPE = ( "\x" {HEX} {HEX} | "\u\{" {HEX}+ "}" | "\\" [nr\\t'\"] )
CHAR_CHAR = ({CHAR_ESCAPE} | [^\\'\n])
STRING_CHAR = ({CHAR_ESCAPE} | [^\\\"\n])
LINE_COMMENT = "//"[^\n]*
LINE_STRING = ("\\" [^\n]* [ \n]*)+

CHAR_LITERAL = "'" {CHAR_CHAR} "'"
INCOMPLETE_CHAR = "'" {CHAR_CHAR}
FLOAT_LITERAL = (
      "0x" {HEX}+ "." {HEX}+  ([pP] [-+]? {HEX}+)?
    |      [0-9]+ "." [0-9]+  ([eE] [-+]? [0-9]+)?
    | "0x" {HEX}+ "."?         [pP] [-+]? {HEX}+
    |      [0-9]+ "."?         [eE] [-+]? [0-9]+
)
INTEGER_LITERAL = (
      "0b" [01]+
    | "0o" [0-7]+
    | "0x" {HEX}+
    |      [0-9]+
)
STRING_LITERAL_SINGLE = "\"" {STRING_CHAR}* "\""
INCOMPLETE_STRING     = "\"" {STRING_CHAR}*
IDENTIFIER = (// !keyword
      [A-Za-z_] [A-Za-z0-9_]*
    | "@\"" {STRING_CHAR}* "\""
    )
BUILTIN_IDENTIFIER = "@" [A-Za-z_][A-Za-z0-9_]*

WHITESPACE = [ \n]+
INVALID_WHITESPACE = [\s\f\r\t]+

%%

{LINE_COMMENT}       { return ZigTokenType.LINE_COMMENT; }
{WHITESPACE}         { return TokenType.WHITE_SPACE; }
{INVALID_WHITESPACE} { return TokenType.BAD_CHARACTER; } // Is this distinction even necessary?

"&"    { return ZigTypes.AMPERSAND; }
"&="   { return ZigTypes.AMPERSANDEQUAL; }
"*"    { return ZigTypes.ASTERISK; }
"**"   { return ZigTypes.ASTERISK2; }
"*="   { return ZigTypes.ASTERISKEQUAL; }
"*%"   { return ZigTypes.ASTERISKPERCENT; }
"*%="  { return ZigTypes.ASTERISKPERCENTEQUAL; }
"^"    { return ZigTypes.CARET; }
"^="   { return ZigTypes.CARETEQUAL; }
":"    { return ZigTypes.COLON; }
","    { return ZigTypes.COMMA; }
"."    { return ZigTypes.DOT; }
".."   { return ZigTypes.DOT2; }
"..."  { return ZigTypes.DOT3; }
".*"   { return ZigTypes.DOTASTERISK; }
".?"   { return ZigTypes.DOTQUESTIONMARK; }
"="    { return ZigTypes.EQUAL; }
"=="   { return ZigTypes.EQUALEQUAL; }
"=>"   { return ZigTypes.EQUALRARROW; }
"!"    { return ZigTypes.EXCLAMATIONMARK; }
"!="   { return ZigTypes.EXCLAMATIONMARKEQUAL; }
"<"    { return ZigTypes.LARROW; }
"<<"   { return ZigTypes.LARROW2; }
"<<="  { return ZigTypes.LARROW2EQUAL; }
"<="   { return ZigTypes.LARROWEQUAL; }
"{"    { return ZigTypes.LBRACE; }
"["    { return ZigTypes.LBRACKET; }
"("    { return ZigTypes.LPAREN; }
"-"    { return ZigTypes.MINUS; }
"-="   { return ZigTypes.MINUSEQUAL; }
"-%"   { return ZigTypes.MINUSPERCENT; }
"-%="  { return ZigTypes.MINUSPERCENTEQUAL; }
"->"   { return ZigTypes.MINUSRARROW; }
"%"    { return ZigTypes.PERCENT; }
"%="   { return ZigTypes.PERCENTEQUAL; }
"|"    { return ZigTypes.PIPE; }
"||"   { return ZigTypes.PIPE2; }
"|="   { return ZigTypes.PIPEEQUAL; }
"+"    { return ZigTypes.PLUS; }
"++"   { return ZigTypes.PLUS2; }
"+="   { return ZigTypes.PLUSEQUAL; }
"+%"   { return ZigTypes.PLUSPERCENT; }
"+%="  { return ZigTypes.PLUSPERCENTEQUAL; }
"[*c]" { return ZigTypes.PTRC; }
"[*]"  { return ZigTypes.PTRUNKNOWN; }
"?"    { return ZigTypes.QUESTIONMARK; }
">"    { return ZigTypes.RARROW; }
">>"   { return ZigTypes.RARROW2; }
">>="  { return ZigTypes.RARROW2EQUAL; }
">="   { return ZigTypes.RARROWEQUAL; }
"}"    { return ZigTypes.RBRACE; }
"]"    { return ZigTypes.RBRACKET; }
")"    { return ZigTypes.RPAREN; }
";"    { return ZigTypes.SEMICOLON; }
"/"    { return ZigTypes.SLASH; }
"/="   { return ZigTypes.SLASHEQUAL; }
"~"    { return ZigTypes.TILDE; }

align           { return ZigTypes.ALIGN_KEYWORD; }
allowzero       { return ZigTypes.ALLOWZERO_KEYWORD; }
and             { return ZigTypes.AND_KEYWORD; }
anyframe        { return ZigTypes.ANYFRAME_KEYWORD; }
anytype         { return ZigTypes.ANYTYPE_KEYWORD; }
asm             { return ZigTypes.ASM_KEYWORD; }
async           { return ZigTypes.ASYNC_KEYWORD; }
await           { return ZigTypes.AWAIT_KEYWORD; }
break           { return ZigTypes.BREAK_KEYWORD; }
catch           { return ZigTypes.CATCH_KEYWORD; }
comptime        { return ZigTypes.COMPTIME_KEYWORD; }
const           { return ZigTypes.CONST_KEYWORD; }
continue        { return ZigTypes.CONTINUE_KEYWORD; }
defer           { return ZigTypes.DEFER_KEYWORD; }
else            { return ZigTypes.ELSE_KEYWORD; }
enum            { return ZigTypes.ENUM_KEYWORD; }
errdefer        { return ZigTypes.ERRDEFER_KEYWORD; }
error           { return ZigTypes.ERROR_KEYWORD; }
export          { return ZigTypes.EXPORT_KEYWORD; }
extern          { return ZigTypes.EXTERN_KEYWORD; }
false           { return ZigTypes.FALSE_KEYWORD; }
fn              { return ZigTypes.FN_KEYWORD; }
for             { return ZigTypes.FOR_KEYWORD; }
if              { return ZigTypes.IF_KEYWORD; }
inline          { return ZigTypes.INLINE_KEYWORD; }
noalias         { return ZigTypes.NOALIAS_KEYWORD; }
nosuspend       { return ZigTypes.NOSUSPEND_KEYWORD; } // Not in official keyword macro, probably oversight
null            { return ZigTypes.NULL_KEYWORD; }
opaque          { return ZigTypes.OPAQUE_KEYWORD; }
or              { return ZigTypes.OR_KEYWORD; }
orelse          { return ZigTypes.ORELSE_KEYWORD; }
packed          { return ZigTypes.PACKED_KEYWORD; }
pub             { return ZigTypes.PUB_KEYWORD; }
resume          { return ZigTypes.RESUME_KEYWORD; }
return          { return ZigTypes.RETURN_KEYWORD; }
linksection     { return ZigTypes.LINKSECTION_KEYWORD; }
struct          { return ZigTypes.STRUCT_KEYWORD; }
suspend         { return ZigTypes.SUSPEND_KEYWORD; }
switch          { return ZigTypes.SWITCH_KEYWORD; }
test            { return ZigTypes.TEST_KEYWORD; }
threadlocal     { return ZigTypes.THREADLOCAL_KEYWORD; }
true            { return ZigTypes.TRUE_KEYWORD; }
try             { return ZigTypes.TRY_KEYWORD; }
undefined       { return ZigTypes.UNDEFINED_KEYWORD; }
union           { return ZigTypes.UNION_KEYWORD; }
unreachable     { return ZigTypes.UNREACHABLE_KEYWORD; }
usingnamespace  { return ZigTypes.USINGNAMESPACE_KEYWORD; }
var             { return ZigTypes.VAR_KEYWORD; }
volatile        { return ZigTypes.VOLATILE_KEYWORD; }
while           { return ZigTypes.WHILE_KEYWORD; }


{CHAR_LITERAL}          { return ZigTypes.CHAR_LITERAL; }
{STRING_LITERAL_SINGLE} { return ZigTypes.STRING_LITERAL_SINGLE; }
{LINE_STRING}           { return ZigTypes.LINE_STRING; }

{INCOMPLETE_STRING}     { return TokenType.BAD_CHARACTER; }
{INCOMPLETE_CHAR}       { return TokenType.BAD_CHARACTER; }

{INTEGER_LITERAL}       { return ZigTypes.INTEGER_LITERAL; }
{FLOAT_LITERAL}         { return ZigTypes.FLOAT_LITERAL; }
{IDENTIFIER}            { return ZigTypes.IDENTIFIER; }
{BUILTIN_IDENTIFIER}    { return ZigTypes.BUILTIN_IDENTIFIER; }

{OTHERWISE}             { return TokenType.BAD_CHARACTER; }
