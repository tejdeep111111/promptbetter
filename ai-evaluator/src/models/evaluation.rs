use serde::{Deserialize, Deserializer, Serialize};

fn bool_or_int<'de, D>(deserializer: D) -> Result<bool, D::Error>
where
    D: Deserializer<'de>,
{
    #[derive(Deserialize)]
    #[serde(untagged)]
    enum BoolOrInt {
        Bool(bool),
        Int(i64),
        Str(String),
    }

    match BoolOrInt::deserialize(deserializer)? {
        BoolOrInt::Bool(b) => Ok(b),
        BoolOrInt::Int(i) => Ok(i != 0),
        BoolOrInt::Str(s) => Ok(s.eq_ignore_ascii_case("true") || s == "1"),
    }
}

#[derive(Debug, Deserialize)]
pub struct EvaluationRequest {
    pub user_prompt: String,
    pub domain: Option<String>,
    pub title: Option<String>,
    pub task: Option<String>,
    pub difficulty: Option<String>,
    pub teaching_point: Option<String>,
    pub evaluation_guide: Option<String>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct EvaluationResponse {
    pub score: i32,
    pub final_score: i32,
    pub clarity: i32,
    pub specificity: i32,
    pub context: i32,
    pub teaching_point_met: bool,
    pub strengths: Vec<String>,
    pub flaws: Vec<String>,
    pub improved_prompt: String,
    pub explanation: String,
    pub evaluated_by: String,
    pub dimensions: EvaluationDimensions,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct EvaluationDimensions {
    pub clarity: i32,
    pub specificity: i32,
    pub context: i32,
}

#[derive(Debug, Deserialize, Default)]
pub struct LlmVerdict {
    #[serde(default)]
    pub clarity: i32,
    #[serde(default)]
    pub specificity: i32,
    #[serde(default)]
    pub context: i32,
    #[serde(default, deserialize_with = "bool_or_int")]
    pub teaching_point_met: bool,
    #[serde(default)]
    pub strengths: Vec<String>,
    #[serde(default)]
    pub flaws: Vec<String>,
    #[serde(default)]
    pub improved_prompt: String,
    #[serde(default)]
    pub explanation: String,
}